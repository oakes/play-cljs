(set-env!
  :resource-paths #{"src" "resources"}
  :dependencies '[[adzerk/boot-cljs "2.1.4" :scope "test"]
                  [adzerk/boot-reload "0.5.2" :scope "test"]
                  [org.clojure/clojurescript "1.9.946" :scope "provided"]
                  [org.clojure/core.async "0.3.443"]
                  [dynadoc "1.1.6" :scope "test"]
                  [defexample "1.6.1"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(require
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-reload :refer [reload]]
  '[dynadoc.boot :refer [dynadoc]])

(task-options!
  pom {:project 'play-cljs
       :version "0.11.3-SNAPSHOT"
       :description "A ClojureScript game library"
       :url "https://github.com/oakes/play-cljs"
       :license {"Public Domain" "http://unlicense.org/UNLICENSE"}}
  push {:repo "clojars"})

(deftask run-docs []
  (set-env! :source-paths #{"src"} :resource-paths #{"dev-resources" "resources"})
  (comp
    (watch)
    (reload :asset-path "dynadoc-extend")
    (cljs)
    (dynadoc :port 5000)))

(deftask local []
  (comp (pom) (jar) (install)))

(deftask deploy []
  (comp (pom) (jar) (push)))

