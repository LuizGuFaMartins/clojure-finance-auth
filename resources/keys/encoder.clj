(require '[clojure.java.io :as io])
(import '(java.util Base64))

(defn file-to-base64 [path]
  (let [file (io/file path)]
    (if (.exists file)
      (let [content (slurp file)
            encoder (Base64/getEncoder)]
        (.encodeToString encoder (.getBytes content)))
      (str "ERRO: Arquivo não encontrado em " path))))

;; Configuração dos seus caminhos
(let [priv-path "jwt-sign-key.pem"
      pub-path  "jwt-sign-key-pub.pem"] ;; Ajuste o nome se for public.pem

  (println "\n--- COPIE PARA O SEU .ENV ---\n")
  (println (str "JWT_PRIVATE_KEY_B64=" (file-to-base64 priv-path)))
  (println "")
  (println (str "JWT_PUBLIC_KEY_B64=" (file-to-base64 pub-path)))
  (println "\n-----------------------------\n"))