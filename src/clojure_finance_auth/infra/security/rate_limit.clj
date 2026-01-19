(ns clojure-finance-auth.infra.security.rate-limit
  (:require [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.chain :as chain]))

(defonce ip-cache (atom {}))

(def rate-limit-interceptor
  (interceptor
    {:name ::rate-limit
     :enter (fn [ctx]
              (let [ip (get-in ctx [:request :remote-addr] "unknown")
                    now (System/currentTimeMillis)
                    window 30000 ; 30 segundos
                    limit 100    ; (100 requisições)

                    requests (filter #(> % (- now window)) (get @ip-cache ip []))
                    new-requests (conj requests now)]

                (swap! ip-cache assoc ip new-requests)

                (if (> (count new-requests) limit)
                  (-> ctx
                      (assoc :response {:status 429
                                        :headers {"Content-Type" "application/json"}
                                        :body {:error "Too Many Requests"}})
                      chain/terminate)
                  ctx
                ))
              )}))