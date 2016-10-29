(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
                  [adzerk/boot-reload "0.4.12" :scope "test"]
                  [pandeiro/boot-http "0.7.3" :scope "test"]
                  ; project deps
                  [org.clojure/clojurescript "1.9.225"]
                  [play-cljs "0.6.5"]])

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]])

(deftask run []
  (comp
    (serve :dir "target/public")
    (watch)
    (reload)
    (cljs :source-map true :optimizations :none)
    (target)))

(deftask build []
  (comp (cljs :optimizations :simple) (target)))

