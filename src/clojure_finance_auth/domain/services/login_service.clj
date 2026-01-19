(ns clojure-finance-auth.domain.services.login-service
  (:require
    [clojure-finance-auth.domain.services.user-service :as user-service]
    [clojure-finance-auth.infra.security.jwt :as jwt]
    [clojure-finance-auth.infra.security.hash :as hash]))

(defn authenticate
  [ctx {:keys [email password]}]
  (let [result (user-service/find-user-by-email-with-password ctx email)
        user (:success result)]
    (cond
      (nil? user)
      {:error :user-not-found}

      (not (hash/check-password password (:password user)))
      {:error :invalid-password}

      (not (:active user))
      {:error :user-inactive}

      :else
      (let [token (jwt/create-token ctx user)
            user-data (dissoc user :password)]
        {:success {:access-token token
                   :user user-data}}))))
