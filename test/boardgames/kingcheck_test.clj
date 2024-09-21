(ns boardgames.kingcheck-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [boardgames.utils :as u]
            [boardgames.test-utils :as t]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))

(deftest chess-king-test

  (t/expect-chess-moves {:piece :P}
                    ;; Cannot move P, it's pinned by r
                    '[[- - - - -]
                      [- - - - -]
                      [K P - r -]
                      [- - - - -]
                      [- - - - -]]

                    '[])
  (t/expect-chess-moves {:piece :K}
                    ;; K cannot move forward
                    '[[- - - - -]
                      [- - - r -]
                      [K - - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -] [- - - - -]
                      [- - - r -] [- - - r -] [- - - r -]
                      [- - - - -] [- - - - -] [- K - - -]
                      [K - - - -] [- K - - -] [- - - - -]
                      [- - - - -] [- - - - -] [- - - - -]])

  (t/expect-chess-moves {:piece :p} ;; lower case sets the turn to black
                    ;; Cannot move p, it's pinned by R
                    '[[- - - - -]
                      [k p - - R]
                      [- - - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[])

  (t/expect-chess-moves {:piece :k}

                    '[[k - - - -]
                      [- - - - -]
                      [P P - - -]
                      [- - - - -]
                      [- - - - -]]

                    '[[- k - - -]
                      [- - - - -]
                      [P P - - -]
                      [- - - - -]
                      [- - - - -]]))



#_#_^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
    (clerk/add-viewers! [viewers/board-viewer viewers/board-move-viewer viewers/side-by-side-move-viewer])

  ^{:nextjournal.clerk/visibility {:code :hide :result :show}}
  (t/view-test-case (t/expect-moves-2 {:piece :P
                                       :extra-checks [{} {} {:captures '(:p)}]}

                                      '[[- - - - -]
                                        [- - - - -]
                                        [- - p - -]
                                        [- P - - -]
                                        [- - - - -]]

                                      '[[- - - - -] [- - - - -] [- - - - -]
                                        [- - - - -] [- P - - -] [- - - - -]
                                        [- P p - -] [- - p - -] [- - P Q -]
                                        [- - - - -] [- - - - -] [- - - - -]
                                        [- - - - -] [- - - - -] [- - - - -]]))
