(ns clojure-finance-auth.infra.components.datasource-component
    (:require
      [com.stuartsierra.component :as component]
      [clojure.string :as str])
    (:import
      (com.zaxxer.hikari HikariConfig HikariDataSource)
      (org.flywaydb.core Flyway)))

(defn run-flyway! [jdbc-url]
      (-> (Flyway/configure)
          (.dataSource jdbc-url nil nil)
          (.load)
          (.migrate)))

(defn hikari-datasource [jdbc-url]
      (let [cfg (doto (HikariConfig.)
                      (.setJdbcUrl jdbc-url)
                      (.setMaximumPoolSize 10))]
           (HikariDataSource. cfg)))

(defrecord DatasourceComponent [config datasource]
           component/Lifecycle

           (start [this]
                  (let [db-url (get-in config [:db :url])]
                       (when (str/blank? db-url)
                             (throw (ex-info "DATABASE_URL not defined" {})))

                       (run-flyway! db-url)

                       (assoc this :datasource (hikari-datasource db-url))))

           (stop [this]
                 (when-let [ds (:datasource this)]
                           (.close ds))
                 (assoc this :datasource nil)))

(defn new-datasource-component [config]
      (map->DatasourceComponent {:config config}))
