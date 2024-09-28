(ns boardgames.simple-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [boardgames.utils :as u]
            [boardgames.test-utils :as t]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))



(deftest ^:ignore-failure failing-on-purpose

  (t/expect-chess-moves {:piece :R}
                        '[[p R -]
                          [- - -]
                          [- K -]]

                        '[[R - -] [p - -] [p - R]
                          [- - -] [- - -] [- - -]
                          [- K -] [- K -] [- K -]]))
