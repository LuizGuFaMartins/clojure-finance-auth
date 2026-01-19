(ns clojure-finance-auth.infra.http.context-utils)

(defn get-db
  "Retorna a conexão ativa (transação) ou o datasource como fallback."
  [ctx]
  (or (get-in ctx [:request :tx-conn])
      (get-in ctx [:components :datasource])))

(defn get-identity
  "Retorna os dados do usuário logado (claims do JWT)."
  [ctx]
  (get-in ctx [:request :identity]))

(defn get-user-id
  "Retorna apenas o ID do usuário logado."
  [ctx]
  (get-in ctx [:request :identity :id]))