(ns clojure-finance-auth.infra.security.rls
  (:require [next.jdbc :as jdbc]))

(def rls-interceptor
  {:name :rls-interceptor
   :enter (fn [context]
            (let [user-id (get-in context [:request :identity :id])
                  datasource (get-in context [:components :datasource])]
              (if user-id
                (let [conn (jdbc/get-connection datasource)]
                  (.setAutoCommit conn false)
                  (try
                    (jdbc/execute! conn [(str "SET LOCAL app.current_user_id = '" user-id "'")])
                    (assoc-in context [:request :tx-conn] conn)
                    (catch Exception e
                      (.close conn)
                      (throw e))))
                context)))

   :leave (fn [context]
            (when-let [conn (get-in context [:request :tx-conn])]
              (.commit conn)
              (.close conn))
            context)

   :error (fn [context err]
            (when-let [conn (get-in context [:request :tx-conn])]
              (.rollback conn)
              (.close conn))
            (assoc context :io.pedestal.interceptor.chain/error err))})