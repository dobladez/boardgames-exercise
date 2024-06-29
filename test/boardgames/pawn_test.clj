(ns boardgames.pawn-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [boardgames.utils :as u]
            [boardgames.test-utils :as t]
            [boardgames.clerk-viewers :as viewers]
            [boardgames.core :as core]
            [boardgames.chess :as chess]
            [nextjournal.clerk :as clerk]))

(deftest chess-pawn-basic-moves

  (t/expect-moves-2 {:piece :P}

                    '[[- - - - -]
                      [- - - R -]
                      [- - - - -]
                      [- P - P -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -] [- - - - -]
                      [- - - R -] [- P - R -] [- - - R -]
                      [- P - - -] [- - - - -] [- - - P -]
                      [- - - P -] [- - - P -] [- P - - -]
                      [- - - - -] [- - - - -] [- - - - -]])

  (t/expect-moves-2 {:piece :p} ;; lower case sets the turn to black

                    '[[- - - - -]
                      [- p - - -]
                      [- - - - -]
                      [- - - - -]
                      [- - - - -]]

                  ;; Reminder: boards expected to be sorted by X and then Y position of moving piece
                    '[[- - - - -] [- - - - -]
                      [- - - - -] [- - - - -]
                      [- - - - -] [- p - - -]
                      [- p - - -] [- - - - -]
                      [- - - - -] [- - - - -]])

  ;; Test for already moved pawn
  (t/expect-moves-2 {:piece :P
                     :update-board-fn t/flag-all-pieces-moved}

                    '[[- - - - -]
                      [- - - - -]
                      [- - - - -]
                      [- P - - -]
                      [- - - - -]]

                    '[[- - - - -]
                      [- - - - -]
                      [- P - - -]
                      [- - - - -]
                      [- - - - -]]))

(deftest chess-pawn-capturing-moves

  (t/expect-moves-2 {:piece :P
                     :extra-checks [{} {} {:captures '(:p)}]}

                    '[[- - - - -]
                      [- - - - -]
                      [- - p - -]
                      [- P - - -]
                      [- - - - -]]

                    '[[- - - - -] [- - - - -] [- - - - -]
                      [- - - - -] [- P - - -] [- - - - -]
                      [- P p - -] [- - p - -] [- - P - -]
                      [- - - - -] [- - - - -] [- - - - -]
                      [- - - - -] [- - - - -] [- - - - -]]))

^{:nextjournal.clerk/visibility {:code :hide :result :hide}}
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
