(set-env!
  :dependencies '[[adzerk/boot-cljs "2.1.4" :scope "test"]
                  [adzerk/boot-reload "0.5.2" :scope "test"]
                  [dynadoc "1.2.0" :scope "test"]
                  [org.clojars.oakes/boot-tools-deps "0.1.4.1" :scope "test"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[dynadoc.boot :refer [dynadoc]]
  '[boot-tools-deps.core :refer [deps]])

(task-options!
  pom {:project 'play-cljs
       :version "0.12.1-SNAPSHOT"
       :description "A ClojureScript game library"
       :url "https://github.com/oakes/play-cljs"
       :license {"Public Domain" "http://unlicense.org/UNLICENSE"}}
  push {:repo "clojars"})

(deftask run-docs []
  (set-env! :resource-paths #{"dev-resources" "resources"})
  (comp
    (deps :aliases [:cljs])
    (watch)
    (reload
      :on-jsload 'dynadoc.core/reload
      :asset-path "dynadoc-extend")
    (cljs
      :optimizations :none
      :compiler-options {:asset-path "/main.out"})
    (dynadoc :port 5000)))

(deftask local []
  (comp (deps) (pom) (jar) (install)))

(deftask deploy []
  (comp (deps) (pom) (jar) (push)))

