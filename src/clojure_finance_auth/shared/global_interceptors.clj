(ns clojure-finance-auth.shared.global-interceptors
  (:require
    [io.pedestal.interceptor :refer [interceptor]]
    [io.pedestal.http.ring-middlewares :as middlewares]
    [io.pedestal.http.content-negotiation :as content-negotiation]))

(defn inject-components
  [components]
  (interceptor
    {:name ::inject-components
     :enter (fn [ctx]
              (assoc ctx :components components))}))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["application/json"]))

(def cookies-interceptor middlewares/cookies)
