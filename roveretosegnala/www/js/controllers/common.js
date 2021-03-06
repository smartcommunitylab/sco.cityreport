angular.module('roveretoSegnala.controllers.common', [])

.controller('AppCtrl', function ($scope, $ionicModal, $ionicHistory, segnalaService, $timeout, Toast, $filter, archiveService, $state, Config) {
    /*    // Form data for the login modal
        $scope.loginData = {};

        // Create the login modal that we will use later
        $ionicModal.fromTemplateUrl('templates/login.html', {
            scope: $scope
        }).then(function (modal) {
            $scope.modal = modal;
        });

        // Triggered in the login modal to close it
        $scope.closeLogin = function () {
            $scope.modal.hide();
        };

        // Open the login modal
        $scope.login = function () {
            $scope.modal.show();
        };

        // Perform the login action when the user submits the login form
        $scope.doLogin = function () {
            console.log('Doing login', $scope.loginData);

            // Simulate a login delay. Remove this and replace with your login
            // code if using a login system
            $timeout(function () {
                $scope.closeLogin();
            }, 1000);
        };*/


    $scope.version = Config.getVersion();
    // Modal 1
    $ionicModal.fromTemplateUrl('templates/login.html', {
        id: '1', // We need to use and ID to identify the modal that is firing the event!
        scope: $scope,
        backdropClickToClose: false,
        animation: 'slide-in-up'
    }).then(function (modal) {
        $scope.oModal1 = modal;
    });

    // Modal 2
    $ionicModal.fromTemplateUrl('templates/credits.html', {
        id: '2', // We need to use and ID to identify the modal that is firing the event!
        scope: $scope,
        backdropClickToClose: false,
        animation: 'slide-in-up'
    }).then(function (modal) {
        $scope.oModal2 = modal;
    });

    $scope.openMap = function () {
        //reset poi to zoom before open zoom
        archiveService.resetMapCenterForSignal();
        if (!$state.is('app.map')) {
            $state.go('app.map');
        } else {
            $state.reload();
        }
    }
    $scope.startRatingSurvey = function () {
        startRatingSurvey(true);
    }
    $scope.openModal = function (index) {
        if (index == 1) $scope.oModal1.show();
        else $scope.oModal2.show();
    };

    $scope.closeModal = function (index) {
        if (index == 1) $scope.oModal1.hide();
        else $scope.oModal2.hide();
    };

    $scope.openSignal = function () {
            segnalaService.setSignal(null);
            window.location.assign("#/app/segnala/");
                        $ionicHistory.nextViewOptions({
                            disableAnimate: true,
                            disableBack: true
                        });
        }
        /* Listen for broadcasted messages */
    $scope.openLoginPopUp = function () {
        Toast.show($filter('translate')("toast_must_login"), "short", "bottom");


    }
    $scope.$on('modal.shown', function (event, modal) {
        console.log('Modal ' + modal.id + ' is shown!');
    });

    $scope.$on('modal.hidden', function (event, modal) {
        console.log('Modal ' + modal.id + ' is hidden!');
    });

    // Cleanup the modals when we're done with them (i.e: state change)
    // Angular will broadcast a $destroy event just before tearing down a scope 
    // and removing the scope from its parent.
    $scope.$on('$destroy', function () {
        console.log('Destroying modals...');
        $scope.oModal1.remove();
        $scope.oModal2.remove();
    });
})


function showNoPlace() {
    var alertPopup = $ionicPopup.alert({
        title: $filter('translate')("signal_send_no_place_title"),
        template: $filter('translate')("signal_send_no_place_template"),
        buttons: [
            {
                text: $filter('translate')("signal_send_toast_alarm"),
                type: 'button-custom'
                            }
            ]
    });
    alertPopup.then(function (res) {
        console.log('no place');
    });
};

function showNoConnection() {
    var alertPopup = $ionicPopup.alert({
        title: $filter('translate')("signal_send_no_connection_title"),
        template: $filter('translate')("signal_send_no_connection_template"),
        buttons: [
            {
                text: $filter('translate')("signal_send_toast_alarm"),
                type: 'button-custom'
                            }
            ]
    });
    alertPopup.then(function (res) {
        console.log('no place');
    });
};




function handleNoGeolocation(errorFlag) {
    if (errorFlag) {
        var content = 'Error: The Geolocation service failed.';
    } else {
        var content = 'Error: Your browser doesn\'t support geolocation.';
    }

    var options = {
        map: map,
        position: new google.maps.LatLng(60, 105),
        content: content
    };

    var infowindow = new google.maps.InfoWindow(options);
    map.setCenter(options.position);
}
