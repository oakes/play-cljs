(ns play-cljs.core)

; although you could accomplish this with a function by using `apply`,
; i'm using a macro because `apply` is slow
(defmacro run-on-all-screens! [game f & other-args]
  `(let [state# (get-state ~game)
         screens# (get-screens ~game)]
     (run! #(process-commands! ~game (~f % state# ~@other-args)) screens#)))

