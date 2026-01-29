(ns clojure-finance-auth.infra.components.auth-component
  (:require [com.stuartsierra.component :as component]
            [buddy.core.keys :as keys]
            [clojure.java.io :as io])
  (:import (java.security KeyPairGenerator)
           (java.util Base64)))

(defn- generate-key-pair! [priv-file pub-file]
  (println (str "[DEV ENVIRONMENT] Generating Key Pair: " (.getName priv-file)))
  (let [kpg (KeyPairGenerator/getInstance "RSA")
        _ (.initialize kpg 2048)
        kp (.generateKeyPair kpg)
        encoder (Base64/getMimeEncoder 64 (byte-array [10]))]
    (.mkdirs (.getParentFile priv-file))
    (spit priv-file (str "-----BEGIN PRIVATE KEY-----\n"
                         (.encodeToString encoder (.getEncoded (.getPrivate kp)))
                         "\n-----END PRIVATE KEY-----"))
    (spit pub-file (str "-----BEGIN PUBLIC KEY-----\n"
                        (.encodeToString encoder (.getEncoded (.getPublic kp)))
                        "\n-----END PUBLIC KEY-----"))))

(defn- decode-base64-key [b64-str]
  (when b64-str
    (.decode (Base64/getDecoder) ^String b64-str)))

(defn- load-from-b64 [id priv-b64 pub-b64]
  (when (and priv-b64 pub-b64)
    (println (str "Loading keys from Secret Manager (Base64) for ID: " id))
    {:id          id
     :private-key (keys/private-key (decode-base64-key priv-b64))
     :public-key  (keys/public-key (decode-base64-key pub-b64))}))

(defn- load-from-files [keys-dir id filename]
  (let [base-name   (str keys-dir "/" filename)
        private-key (io/file (str base-name ".pem"))
        public-key  (io/file (str base-name "-pub.pem"))]
    (when (and (.exists private-key) (.exists public-key))
      (println (str "Loading keys from Filesystem for ID: " id))
      {:id          id
       :private-key (keys/private-key private-key)
       :public-key  (keys/public-key public-key)})))

(defn- load-or-generate-pair [keys-dir profile {:keys [id filename private-key-b64 public-key-b64]}]
  (let [from-sm (load-from-b64 id private-key-b64 public-key-b64)]
    (or from-sm
        (load-from-files keys-dir id filename)
        (if (= profile :dev)
          (let [base-name (str keys-dir "/" filename)
                priv-file (io/file (str base-name ".pem"))
                pub-file  (io/file (str base-name "-pub.pem"))]
            (generate-key-pair! priv-file pub-file)
            {:id id :private-key (keys/private-key priv-file) :public-key (keys/public-key pub-file)})
          (throw (ex-info "Security keys not found! Provide Base64 environment variables or mount .pem files."
                          {:key-id id :profile profile}))))))

(defrecord AuthComponent [config]
  component/Lifecycle
  (start [this]
    (println "Starting AuthComponent")
    (let [auth-conf    (:auth config)
          profile      (:profile config)
          keys-dir     (:keys-dir auth-conf)
          pairs-def    (:key-pairs auth-conf)
          loaded-pairs (map #(load-or-generate-pair keys-dir profile %) pairs-def)
          keys-map     (reduce (fn [m pair]
                                 (assoc m (:id pair) (dissoc pair :id)))
                               {}
                               loaded-pairs)]
      (assoc this :keys keys-map)))

  (stop [this]
    (println "Stopping AuthComponent")
    (assoc this :keys nil)))

(defn new-auth-component [config]
  (map->AuthComponent {:config config}))