(ns clojure-finance-auth.domain.repositories.user-repo
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [honey.sql :as sql]))

(def builder {:builder-fn rs/as-unqualified-kebab-maps})

(def base-user-query
  {:select [:u.* [:r.name :role]]
   :from [[:users :u]]
   :join [[:user_roles :ur] [:= :u.id :ur.user_id]
          [:roles :r] [:= :ur.role_id :r.id]]})

(defn find-user-by-id [ds id]
  (jdbc/execute-one!
    ds
    (sql/format
      (merge base-user-query
             {:where [:= :u.id id]}))
    builder))

(defn find-user-by-email [ds email]
  (jdbc/execute-one!
    ds
    (sql/format
      (merge base-user-query
             {:where [:= :u.email email]}))
    builder))
