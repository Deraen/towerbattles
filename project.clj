(defproject towerbattles "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.395" :exclusions [org.clojure/tools.reader]]

                 [reagent "0.6.0"]]

  :plugins [[lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [deraen/lein-sass4clj "0.3.0"]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {:on-jsload "towerbattles.core/on-js-reload"
                           :open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main towerbattles.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/towerbattles.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}]}

  :figwheel {:http-server-root "public"
             :css-dirs ["resources/public/css"]}

  :sass {:source-paths ["src"]
         :target-path "resources/public/css"}

  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.3"]
                                  [figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "dev"]
                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
