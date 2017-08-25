(ns teletrofono.utils)

(defmacro child-ns
  ([name]
   `(child-ns ~name ~(symbol (str (ns-name *ns*) "." name))))
  ([name ns]
   `(do (create-ns '~ns)
        (alias '~name '~ns))))

(defn update-with [m update-fn & args]
  (apply update-fn m (concat (butlast args) [((last args) m)])))
