(ns clojure-finance-auth.infra.security.hash
  (:require [buddy.hashers :as hs]))

(defn check-password
  [attempt-password hashed-password]
  (hs/check attempt-password hashed-password))

(defn hash-password
  [password]
  (hs/derive password {:alg :bcrypt+sha512 :iterations 12}))
