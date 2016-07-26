(set-env!
  :resource-paths #{"src"}
  :dependencies '[[org.clojure/clojure "1.9.0-alpha10"]
                  [org.clojure/clojurescript "1.9.89"]]
  :repositories (conj (get-env :repositories)
                  ["clojars" {:url "https://clojars.org/repo/"
                              :username (System/getenv "CLOJARS_USER")
                              :password (System/getenv "CLOJARS_PASS")}]))

(task-options!
  pom {:project 'play-cljs
       :version "1.0.0-SNAPSHOT"}
  jar {:manifest {"Description" "A ClojureScript game library"
                  "Url" "https://github.com/oakes/play-cljs"}}
  push {:repo "clojars"})

(deftask try []
  (comp (pom) (jar) (install)))

(deftask deploy []
  (comp (pom) (jar) (push)))

