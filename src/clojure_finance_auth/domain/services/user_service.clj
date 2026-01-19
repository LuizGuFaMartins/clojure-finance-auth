(ns clojure-finance-auth.domain.services.user-service
  (:require
    [clojure-finance-auth.domain.repositories.user-repo :as repo]
    [clojure-finance-auth.infra.http.context-utils :as ctx-utils]))

(defn find-user-by-id
  [ctx id]
  (let [conn (ctx-utils/get-db ctx)
        user (repo/find-user-by-id conn id)]
    (if user
      {:success (dissoc user :password)}
      {:error :user-not-found})))

(defn find-user-by-email-with-password
  [ctx email]
  (let [conn (ctx-utils/get-db ctx)
        user (repo/find-user-by-email conn email)]
    (if user
      {:success user}
      {:error :user-not-found})))
