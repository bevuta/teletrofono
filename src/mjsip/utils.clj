(ns mjsip.utils)

(defmacro child-ns
  ([name]
   `(child-ns ~name ~(symbol (str (ns-name *ns*) "." name))))
  ([name ns]
   `(do (create-ns '~ns)
        (alias '~name '~ns))))
