(ns clojure-finance-auth.infra.components.pedestal-component
  (:require
    [com.stuartsierra.component :as component]
    [io.pedestal.http :as http]
    [clojure-finance-auth.shared.global-interceptors :as interceptors]
    [clojure-finance-auth.infra.http.routes :refer [routes]]
    [clojure-finance-auth.infra.security.rate-limit :as rate-limit]
    [io.pedestal.http.cors :as cors]))

(defrecord PedestalComponent [config datasource auth]
           component/Lifecycle

    (start [component]
           (println "Starting PedestalComponent")
           (let [service-map {::http/router :linear-search
                              ::http/routes routes
                              ::http/type :jetty
                              ::http/join? false
                              ::http/port (-> config :server :port)
                              ::http/components {:datasource datasource :auth auth}}

                 cors-interceptor (cors/allow-origin {:creds true
                                                      :allowed-origins ["http://localhost:3000" "http://127.0.0.1:3000"]})

                 server (-> service-map
                            http/default-interceptors
                            (update ::http/interceptors (fn [stack]
                                                          (into [
                                                                 cors-interceptor
                                                                 interceptors/cookies-interceptor
                                                                 interceptors/content-negotiation-interceptor
                                                                 rate-limit/rate-limit-interceptor
                                                                 (interceptors/inject-components component)]
                                                                stack)))
                            http/create-server
                            http/start)]
             (assoc component :server server)))

         (stop [component]
               (println "Stopping PedestalComponent")
               (when-let [server (:server component)]
                         (http/stop server))
               (assoc component :server nil)))

(defn new-pedestal-component [config]
      (map->PedestalComponent {:config config}))
