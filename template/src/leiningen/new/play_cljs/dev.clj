(require
  '[figwheel.main :as figwheel]
  '[nightlight.core :as nightlight]
  '[clojure.java.io :as io])

(defn delete-children-recursively! [f]
  (when (.isDirectory f)
    (doseq [f2 (.listFiles f)]
      (delete-children-recursively! f2)))
  (when (.exists f) (io/delete-file f)))

(delete-children-recursively! (io/file "resources/public/main.out"))

(nightlight/start {:port 4000 :url "http://localhost:9500"})
(figwheel/-main "--build" "dev")
