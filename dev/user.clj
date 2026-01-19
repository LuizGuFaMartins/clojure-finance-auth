(ns user
    (:require
   [clojure-finance-auth.config :as config]
   [clojure-finance-auth.core :as core]
   [clojure.tools.namespace.repl :refer [refresh]]
   [com.stuartsierra.component :as component]))

(def system nil)

(defn init []
  (alter-var-root #'system
                  (constantly
                   (core/clojure-finance-api-system
                    (config/read-config)))))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system
                  (fn [s]
                    (when s
                      (component/stop s)))))

(defn go []
  (init)
  (start))

(defn reset []
  (stop)
  (refresh :after 'user/go))
