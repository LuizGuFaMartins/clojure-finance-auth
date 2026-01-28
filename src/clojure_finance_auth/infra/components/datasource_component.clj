(ns clojure-finance-auth.infra.components.datasource-component
  (:require
    [com.stuartsierra.component :as component])
  (:import
    (com.zaxxer.hikari HikariConfig HikariDataSource)))

(defn- build-jdbc-url [{:keys [host port dbname]}]
  (format "jdbc:postgresql://%s:%s/%s"
          (or host "localhost")
          (or (if (string? port) (Integer/parseInt port) port) 5432)
          (or dbname "finance_db")))

(defn- hikari-datasource [jdbc-url user password]
  (let [cfg (doto (HikariConfig.)
              (.setJdbcUrl jdbc-url)
              (.setUsername (or user "finance_user"))
              (.setPassword (or password "finance_pass"))

              ;; --- CONFIGURAÇÕES DE TIMEOUT ---
              ;; Tempo máximo para conseguir uma conexão do pool (30 segundos)
              (.setConnectionTimeout 30000)
              ;; Tempo máximo que uma conexão pode ficar aberta (30 minutos)
              (.setMaxLifetime 1800000)
              ;; Tamanho do pool
              (.setMaximumPoolSize 10)

              ;; --- SEGURANÇA NO POSTGRES ---
              ;; Força o Postgres a matar qualquer query que demore mais de 10 segundos
              (.addDataSourceProperty "options" "-c statement_timeout=10000")

              ;; Performance
              (.addDataSourceProperty "cachePrepStmts" "true")
              (.addDataSourceProperty "prepStmtCacheSize" "250"))]
    (HikariDataSource. cfg)))

(defrecord DatasourceComponent [config datasource]
  component/Lifecycle

  (start [this]
    (println "Starting DatasourceComponent")
    (let [db-config (:db config)
          jdbc-url  (build-jdbc-url db-config)
          user      (:user db-config)
          password  (:password db-config)]

      (assoc this :datasource (hikari-datasource jdbc-url user password))))

  (stop [this]
    (println "Stopping DatasourceComponent")
    (when-let [ds (:datasource this)]
      (.close ds))
    (assoc this :datasource nil)))

(defn new-datasource-component [config]
  (map->DatasourceComponent {:config config}))