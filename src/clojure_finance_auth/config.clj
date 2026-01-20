(ns clojure-finance-auth.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defn read-config []
  (let [env-str (or (System/getenv "APP_ENV") "dev")
        profile (keyword env-str)]

    (println ">>> Environment config >" (name profile) "<")

    (-> "config.edn"
        (io/resource)
        (aero/read-config {:profile profile})
        (assoc :profile profile)
        )))
