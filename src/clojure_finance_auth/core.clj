(ns clojure-finance-auth.core
  (:require
    [clojure-finance-auth.infra.components.datasource-component :as datasource-component]
    [clojure-finance-auth.infra.components.auth-component :as auth-component]
    [clojure-finance-auth.infra.components.pedestal-component :as pedestal-component]
    [clojure-finance-auth.config :as config]
    [next.jdbc.result-set :as rs]
    [com.stuartsierra.component :as component]))

(extend-protocol rs/ReadableColumn
  java.util.UUID
  (read-column-by-label [v _] (str v))
  (read-column-by-index [v _ _] (str v))

  java.math.BigDecimal
  (read-column-by-label [v _] (double v))
  (read-column-by-index [v _ _] (double v)))

(defn clojure-finance-api-system
  [config]
  (component/system-map
    :auth (auth-component/new-auth-component config)
    :datasource (datasource-component/new-datasource-component config)
    :pedestal-component (component/using (pedestal-component/new-pedestal-component config) [:datasource :auth]))
  )

(defn -main
  []
  (let [system (-> (config/read-config)
                   (clojure-finance-api-system)
                   (component/start-system))]

    (println "Starting Clojure Finance API Service")

    (.addShutdownHook
      (Runtime/getRuntime)
      (new Thread #(component/stop-system system)))))