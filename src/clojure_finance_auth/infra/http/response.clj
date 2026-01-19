(ns clojure-finance-auth.infra.http.response
  (:require [cheshire.core :as json]))

(defn response
  ([status]
   (response status nil))
  ([status body]
   (try
     (let [encoded-body (if (nil? body)
                          ""
                          (json/encode body))]
       {:status  status
        :headers {"Content-Type" "application/json"}
        :body    encoded-body})
     (catch Exception e
       (let [error-msg (.getMessage e)]
         (println "ERRO DE SERIALIZAÇÃO JSON:" error-msg)
         {:status  500
          :headers {"Content-Type" "application/json"}
          :body    (json/encode {:error   "Internal Server Error"
                                 :message "Falha ao serializar resposta JSON"
                                 :detail  error-msg
                                 :type    (str (type e))})})))))

  (defn response-error
    ([status message]
     (response status {:error message}))
    ([status message details]
     (response status {:error message :details details})))