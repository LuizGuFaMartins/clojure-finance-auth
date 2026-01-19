(ns clojure-finance-auth.infra.components.auth-component
  (:require [com.stuartsierra.component :as component]
            [buddy.core.keys :as keys]
            [clojure.java.io :as io]))

(defrecord AuthComponent [config]
  component/Lifecycle
  (start [this]
    (println "Starting AuthComponent")
    (let [auth-map  (:auth config)
          priv-res  (io/resource (:private-key-path auth-map))
          pub-res   (io/resource (:public-key-path auth-map))]
      (if (and priv-res pub-res)
        (assoc this
          :private-key (keys/str->private-key (slurp priv-res))
          :public-key  (keys/str->public-key (slurp pub-res)))
        (throw (ex-info "Keys not found" {:auth-map auth-map})))))

  (stop [this]
    (println "Stopping AuthComponent")
    (assoc this :private-key nil :public-key nil)))

(defn new-auth-component [config]
  (map->AuthComponent {:config config}))