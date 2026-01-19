(ns clojure-finance-auth.infra.security.jwt
  (:require [buddy.sign.jwt :as jwt]
            [clojure.string :as str]
            [clojure-finance-auth.infra.http.response :refer [response-error]]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.interceptor.chain :as chain]))

(defn auth-error [ctx status message]
  (-> ctx
      (assoc :response (response-error status message))
      chain/terminate))

(defn authz-error [ctx status message]
  (-> ctx
      (assoc :response (response-error status message))
      chain/terminate))

(defn- get-auth-component [ctx]
  (get-in ctx [:components :auth]))

(defn- get-private-key [ctx]
  (:private-key (get-auth-component ctx)))

(defn- get-public-key [ctx]
  (:public-key (get-auth-component ctx)))

(defn- extract-token [ctx]
  (let [auth-header (get-in ctx [:request :headers "authorization"])
        header-token (when (and auth-header (str/starts-with? auth-header "Bearer "))
                       (subs auth-header 7))
        cookies      (get-in ctx [:request :cookies])
        cookie-token (or (get-in cookies ["token" :value])
                         (get-in cookies [:token :value]))]
    (or header-token cookie-token)))

(defn create-token [ctx user]
  (let [priv-key (get-private-key ctx)
        now      (java.time.Instant/now)
        payload  {:id   (str (:id user))
                  :role (name (:role user))
                  :iat  (.getEpochSecond now)
                  :exp  (.getEpochSecond (.plus now 1 java.time.temporal.ChronoUnit/HOURS))
                  :aud  "clojure-finance-api"
                  :type "access"
                  :jti  (str (java.util.UUID/randomUUID))}]
    (jwt/sign payload priv-key {:alg :rs256})))

(defn verify-token [ctx token]
  (let [pub-key (get-public-key ctx)]
    (jwt/unsign token pub-key {:alg :rs256 :aud "clojure-finance-api"})))

(def auth-interceptor
  (interceptor
   {:name ::auth-interceptor
    :enter
    (fn [ctx]
      (let [token (extract-token ctx)]
        (if (str/blank? token)
          (auth-error ctx 401 "Token missing")
          (try
            (let [claims (verify-token ctx token)]
              (if (= (:type claims) "access")
                (assoc-in ctx [:request :identity] claims)
                (auth-error ctx 401 "Invalid token type")))
            (catch Exception _
              (auth-error ctx 401 "Invalid or expired token"))))))}))

(defn authorize-roles
  [allowed-roles]
  (interceptor
   {:name ::authorize-roles
    :enter
    (fn [ctx]
      (let [identity  (get-in ctx [:request :identity])
            user-role (:role identity)
            allowed?  (some #(= (name %) (name user-role)) allowed-roles)]
        (cond
          (nil? identity) (authz-error ctx 401 "Unauthenticated")
          allowed?        ctx
          :else           (authz-error ctx 403 "Forbidden"))))}))