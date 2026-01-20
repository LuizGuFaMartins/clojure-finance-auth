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

(defn- load-or-generate-pair [keys-dir profile {:keys [id filename]}]
  (let [base-name (str keys-dir "/" filename)
        private-key (io/file (str base-name ".pem"))
        public-key  (io/file (str base-name "-pub.pem"))]

    (cond
      (and (.exists private-key) (.exists public-key))
      {:id id :private-key (keys/private-key private-key) :public-key (keys/public-key public-key)}

      (= profile :dev)
      (do
        (generate-key-pair! private-key public-key)
        {:id id :private-key (keys/private-key private-key) :public-key (keys/public-key public-key)})

      :else
      (throw (ex-info "Keys not found" {:key-id id})))))

(defrecord AuthComponent [config]
  component/Lifecycle
  (start [this]
    (println "Starting AuthComponent")
    (let [auth-conf (:auth config)
          profile   (:profile config)
          keys-dir  (:keys-dir auth-conf)
          pairs-def (:key-pairs auth-conf)

          loaded-pairs (map #(load-or-generate-pair keys-dir profile %) pairs-def)

          keys-map (reduce (fn [m pair]
                             (assoc m (:id pair) (dissoc pair :id)))
                           {}
                           loaded-pairs)]

      (assoc this :keys keys-map)))

  (stop [this]
    (assoc this :keys nil)))

(defn new-auth-component [config]
  (map->AuthComponent {:config config}))