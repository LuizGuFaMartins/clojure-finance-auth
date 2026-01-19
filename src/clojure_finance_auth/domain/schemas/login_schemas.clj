(ns clojure-finance-auth.domain.schemas.login-schemas)

(def LoginSchema
  [:map
   [:email string?]
   [:password string?]])

