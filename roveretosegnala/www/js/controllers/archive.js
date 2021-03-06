angular.module('roveretoSegnala.controllers.archive', [])

.controller('ArchiveCtrl', function ($scope, archiveService, $location, Toast, $filter) {

        //log
        Restlogging.appLog("AppConsume", "archive+" + $location.url().substr($location.url().lastIndexOf('/') + 1));
        $scope.listForMap = {};
        $scope.listopen = {};
        $scope.listclosed = {};
        $scope.listprocessing = {};
        $scope.noMoreClosedItemsAvailable = false;
        $scope.noMoreOpenItemsAvailable = false;
        $scope.noMoreProcessingItemsAvailable = false;

        $scope.loadMore = function (state) {

            var length = 0;
            if (state == 'open') {
                if ($scope.listopen.data) {
                    length = $scope.listopen.data.length;
                }
            } else if (state == 'closed') {
                if ($scope.listclosed.data) {
                    length = $scope.listclosed.data.length;
                }
            } else {
                if ($scope.listprocessing.data) {
                    length = $scope.listprocessing.data.length;
                }
            }
            archiveService.getItemsOnStateArchive(state, length).then(function (items) {
                //check state for array
                $scope.emptylist = false;

                if (state == 'open') {
                    if ($scope.listopen.data) {
                        $scope.listopen.data.push.apply($scope.listopen.data, items.data);
                        if (items.data.length < archiveService.getMaxCounter()) {
                            $scope.noMoreOpenItemsAvailable = true;
                        }
                    } else {
                        $scope.listopen = items;
                    }
                    if ($scope.listopen.data.length == 0) {
                        $scope.emptylist = true;
                    } else {
                        $scope.emptylist = false;
                    }
                } else if (state == 'closed') {
                    if ($scope.listclosed.data) {

                        $scope.listclosed.data.push.apply($scope.listclosed.data, items.data);
                        if (items.data.length < archiveService.getMaxCounter()) {

                            $scope.noMoreClosedItemsAvailable = true;
                        }
                    } else {
                        $scope.listclosed = items;
                    }
                    if ($scope.listclosed.data.length == 0) {
                        $scope.emptylist = true;
                    } else {
                        $scope.emptylist = false;
                    }
                } else {
                    if ($scope.listprocessing.data) {
                        $scope.listprocessing.data.push.apply($scope.listprocessing.data, items.data);
                        if (items.data.length < archiveService.getMaxCounter()) {

                            $scope.noMoreProcessingItemsAvailable = true;
                        }
                    } else {
                        $scope.listprocessing = items;
                    }
                    if ($scope.listprocessing.data.length == 0) {
                        $scope.emptylist = true;
                    } else {
                        $scope.emptylist = false;
                    }
                }
                $scope.$broadcast('scroll.infiniteScrollComplete');
            }, function (reason) {
                Toast.show($filter('translate')("network_problem"), "short", "bottom");
                $scope.noMoreOpenItemsAvailable = true;
                $scope.noMoreClosedItemsAvailable = true;
                $scope.noMoreProcessingItemsAvailable = true;
                $scope.emptylist = true;

            });
        }


    })
    .filter('statusstring', function ($filter) {
        return function (string) {
            if (string == 'open') {
                return $filter('translate')('status_open');
            }
            if (string == 'closed') {
                return $filter('translate')('status_closed');
            }
            if (string == 'processing') {
                return $filter('translate')('status_processing');
            }
        };
    })
    .controller('ArchivioDetailCtrl', function ($scope, $stateParams, $filter, $ionicModal, $ionicHistory, archiveService, Config, $state) {
        // "MovieService" is a service returning mock data (services.js)
        Restlogging.appLog("AppConsume", "detail+" + $stateParams.id);

        $scope.signal = archiveService.getItem($stateParams.id);
        $scope.myActiveSlide = 0;
        $scope.categories = Config.getCategories();
        /*
            $scope.title = $filter('translate')("title_ar");
        */
        $scope.goToMap = function () {
            $state.go('app.map');
            //set coordinate
            archiveService.setMapCenterForSignal($scope.signal.location.coordinates[0], $scope.signal.location.coordinates[1]);
        };

        $scope.showImages = function (index) {
            $scope.activeSlide = index;
            $scope.showModal('templates/image-popover.html');
        }

        $scope.showModal = function (templateUrl) {
            $ionicModal.fromTemplateUrl(templateUrl, {
                scope: $scope,
                animation: 'slide-in-up'
            }).then(function (modal) {
                $scope.modal = modal;
                $scope.modal.show();
            });
        }
        $scope.myGoBack = function () {
            $ionicHistory.goBack();
        };
        // Close the modal
        $scope.closeModal = function () {
            $scope.modal.hide();
            $scope.modal.remove()
        };
    })

.controller('MySignalsCtrl', function ($scope, $stateParams, $filter, archiveService) {
        //log
        Restlogging.appLog("AppConsume", "myissues");

        $scope.emptylist = false;
        $scope.mySignals = {};
        $scope.noMoreMySignalsAvailable = false;
        $scope.loadMore = function (state) {
            var length = 0;

            if ($scope.mySignals.data) {
                length = $scope.mySignals.data.length;

            }
            archiveService.getMyItemsArchive(length).then(function (items) {

                if ($scope.mySignals.data) {
                    $scope.mySignals.data.push.apply($scope.mySignals.data, items.data);
                    if (items.data.length < archiveService.getMaxCounter()) {

                        $scope.noMoreMySignalsAvailable = true;
                    }
                } else {
                    $scope.mySignals = items;
                }


                $scope.$broadcast('scroll.infiniteScrollComplete');
                if ($scope.mySignals.data.length == 0) {
                    $scope.emptylist = true;
                } else {
                    $scope.emptylist = false;
                }

            }, function (reason) {
                Toast.show($filter('translate')("network_problem"), "short", "bottom");
                $scope.noMoreMySignalsAvailable = true;
                $scope.emptylist = true;

            });
        }

    })
    .factory('archiveService', function ($http, $q, Config, Toast, $filter, LoginService) {
        var items = null;
        var itemsformap = null;
        var itemsopen = null;
        var itemsclosed = null;
        var itemsprocessing = null;
        var itemsMap = null;
        var counter = '10';
        var mapCenterForSignal = null;
        var itemsService = {};
        itemsService.setMapCenterForSignal = function (lat, long) {
            if (!mapCenterForSignal) {
                mapCenterForSignal = {
                    lat: 0,
                    long: 0
                }
            }
            mapCenterForSignal.lat = lat;
            mapCenterForSignal.long = long

        }
        itemsService.getMapCenterForSignal = function () {
            return mapCenterForSignal;
        }
        itemsService.resetMapCenterForSignal = function () {
            mapCenterForSignal = null;
        }
        itemsService.getMaxCounter = function () {
            return counter;
        }
        itemsService.getItem = function (itemId) {
            return itemsMap[itemId];
        };
        itemsService.listForMap = function () {
            var deferred = $q.defer();
            itemsformap = [];
            itemsService.listForMapByState('open').then(function (data) {
                itemsformap = data;
                itemsService.listForMapByState('closed').then(function (data) {
                    itemsformap.data.push.apply(itemsformap.data, data.data);
                    itemsService.listForMapByState('processing').then(function (data) {
                        itemsformap.data.push.apply(itemsformap.data, data.data);
                        deferred.resolve(itemsformap);
                    })
                })
            }, function (reason) {
                Toast.show($filter('translate')("network_problem"), "short", "bottom");
            });
            return deferred.promise;
        };
        itemsService.listForMapByState = function (state) {
            var deferred = $q.defer();
            //        if (items != null) {
            //            deferred.resolve(items);
            //        } else {
            items = [];
            $http({
                    method: 'GET',
                    url: Config.URL() + '/' + Config.provider() + '/services/' + Config.service() + '/issues?status=' + state + '&count=' + counter

                }).success(function (data) {
                    items = data;
                    if (!itemsMap) {
                        itemsMap = {};
                    }
                    for (var i = 0; i < items.data.length; i++) {
                        itemsMap[items.data[i].id] = items.data[i];
                    }
                    deferred.resolve(items);
                })
                .error(function (err) {
                    deferred.reject(err);
                });
            //        }
            return deferred.promise;
        };

        itemsService.getMyItemsArchive = function (from) {
            var start = null;
            var deferred = $q.defer();

            $http({
                method: 'GET',
                url: Config.URL() + '/' + Config.provider() + '/services/' + Config.service() + '/issues?' + '&user_id=' + LoginService.getUserId() + '&start=' + from + '&count=' + counter

            }).
            success(function (data, status, headers, config) {

                items = data;
                for (var i = 0; i < data.data.length; i++) {
                    itemsMap[data.data[i].id] = data.data[i];
                }
                deferred.resolve(items);
            }).error(function (err) {
                //console.log(data + status + headers + config);
                deferred.reject(err);
            });
            return deferred.promise;

        }

        itemsService.getItemsOnStateArchive = function (state, from) {
            var status = null;
            var start = null;
            var deferred = $q.defer();
            if (state == 'open') {
                status = 'status=open';
            } else if (state == 'closed') {
                status = 'status=closed';
            } else {
                status = 'not_status=open,closed'

            }
            $http({
                method: 'GET',
                url: Config.URL() + '/' + Config.provider() + '/services/' + Config.service() + '/issues?' + status + '&start=' + from + '&count=' + counter

            }).
            success(function (data, status, headers, config) {

                items = data;
                for (var i = 0; i < data.data.length; i++) {
                    itemsMap[data.data[i].id] = data.data[i];
                }
                deferred.resolve(items);
            }).error(function (err) {
                //console.log(data + status + headers + config);
                deferred.reject(err);
            });
            return deferred.promise;

        }
        return itemsService;
    });
