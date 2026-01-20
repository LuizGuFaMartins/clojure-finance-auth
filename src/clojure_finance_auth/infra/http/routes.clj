(ns clojure-finance-auth.infra.http.routes
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [clojure-finance-auth.infra.security.jwt :as jwt]
            [clojure-finance-auth.infra.security.jwks :as jwks]
            [clojure-finance-auth.infra.interceptors.login-interceptors :as login-i]))

(def raw-routes
  [
   ;; --- JWKS (Público) ---
   ["/.well-known/jwks.json" :get [jwks/jwks-interceptor] :route-name :jwks :public true]

   ;; --- Login & Public ---
   ["/login" :post [(body-params/body-params) login-i/login] :route-name :action-login :public true]

   ;; --- Auth & Session ---
   ["/auth/me" :get [login-i/get-current-user] :route-name :auth-me]
   ["/logout" :post [login-i/logout] :route-name :action-logout :public true]
  ])

(defn- wrap-auth-interceptor
  [routes]
  (map
    (fn [route]
      (let [[path method interceptors] route
            opts          (apply hash-map (drop 3 route))
            is-public?    (:public opts)
            roles         (:roles opts)
            clean-opts    (dissoc opts :public :roles :rls)
            auth-chain
              (cond
                is-public?
                interceptors

                :else
                (let [base-chain [jwt/auth-interceptor]]
                  (-> base-chain
                      (cond-> roles (conj (jwt/authorize-roles roles)))
                      (into interceptors))))
            ]

        (vec
          (concat
            [path method auth-chain]
            (mapcat identity clean-opts)))))
    routes))


(def routes
  (-> raw-routes
      wrap-auth-interceptor
      set
      route/expand-routes))