(ns clojure-finance-auth.infra.interceptors.login-interceptors
  (:require
    [clojure-finance-auth.domain.services.user-service :as user-service]
    [malli.core :as m]
    [malli.error :as me]
    [io.pedestal.interceptor :refer [interceptor]]
    [clojure-finance-auth.infra.http.response :refer [response response-error]]
    [clojure-finance-auth.domain.schemas.login-schemas :as schemas]
    [clojure-finance-auth.domain.services.login-service :as login-service]))

(defn error-type-handler [result]
  (let [error-type (:error result)]
     (case error-type
       :user-not-found (response-error 404 "User not found")
       :invalid-password (response-error 401 "Invalid credentials")
       (response-error 500 "Internal Server Error"))))

(def login
  (interceptor
    {:name ::login
     :enter
     (fn [ctx]
       (let [body (get-in ctx [:request :json-params])]
         (if-not (m/validate schemas/LoginSchema body)
           (assoc ctx :response (response-error 400 "Invalid login payload"
                                                (me/humanize (m/explain schemas/LoginSchema body))))

           (let [result (login-service/authenticate ctx body)]
             (if-let [success-data (:success result)]
               (let [token (:access-token success-data)
                     user-data (dissoc success-data :access-token)
                     resp (response 200 (:user user-data))]
                 (assoc ctx :response
                            (assoc resp :cookies {"token" {:value     token
                                                           :http-only true
                                                           :path      "/"
                                                           :same-site :lax ;;:strict
                                                           :secure    false}}))) ;;true
               (assoc ctx :response
                          (if-let [err (:error result)]
                            (error-type-handler result)
                            (response-error 500 "Unknown error"))))))))}))

(def logout
  (interceptor
    {:name ::logout
     :enter
     (fn [ctx]
       (assoc ctx :response
                  (-> (response 200 {:message "Logout realizado com sucesso"})
                      (assoc :cookies {"token" {:value     ""
                                                :http-only true
                                                :path      "/"
                                                :max-age   0
                                                :expires   "Thu, 01 Jan 1970 00:00:00 GMT"
                                                :secure    false}}))))}))

(def get-current-user
  (interceptor
    {:name ::get-current-user
     :enter
     (fn [ctx]
       (let [logged-id  (get-in ctx [:request :identity :id])]

         (if-not logged-id
           (assoc ctx :response (response-error 401 "Não autorizado"))

           (let [uuid-id (java.util.UUID/fromString logged-id)
                 user-result (user-service/find-user-by-id ctx uuid-id)]
             (assoc ctx :response
                        (cond
                          (:success user-result)
                          (response 200 (:success user-result))

                          (:error user-result)
                          (error-type-handler user-result)

                          :else
                          (response-error 500 "Erro desconhecido ao buscar usuário")))))))}))
