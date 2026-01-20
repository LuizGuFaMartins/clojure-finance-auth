(ns clojure-finance-auth.infra.security.jwks
  (:require [clojure-finance-auth.infra.http.response :refer [response]]
            [io.pedestal.interceptor :refer [interceptor]])
  (:import (java.security.interfaces RSAPublicKey)
           (java.util Base64)))

(defn- base64url-encode [byte-array]
  (.encodeToString (Base64/getUrlEncoder) byte-array))

(defn- public-key->jwk [id public-key]
  (when (instance? RSAPublicKey public-key)
    (let [modulus (.getModulus public-key)
          exponent (.getPublicExponent public-key)]
      {:kty "RSA"
       :use "sig"
       :alg "RS256"
       :kid (name id)
       :n   (base64url-encode (.toByteArray modulus))
       :e   (base64url-encode (.toByteArray exponent))})))

(def jwks-interceptor
  (interceptor
    {:name ::jwks
     :enter (fn [ctx]
              (let [auth-component (get-in ctx [:components :auth])
                    keys-map       (:keys auth-component)
                    keys-list      (keep (fn [[id pair]]
                                           (public-key->jwk id (:public-key pair)))
                                         keys-map)]
                (assoc ctx :response (response 200 {:keys keys-list}))))}))