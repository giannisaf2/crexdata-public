<?php

define('__APP_PATH__', dirname(__FILE__));
define('__APP_EXEC__', basename(__FILE__));

// Load classes
require_once(__APP_PATH__ . '/app/autoload.php');

// Application
try {
    $app = new GazeboSimulationApp();
    $app->run();
} catch (Throwable $e) {
    Log::getInstance()->severe('Exception raised | Msg: ' . $e->getMessage() . ' | File: ' . $e->getFile() . ' | Line: ' . $e->getLine());
    Log::getInstance()->severe('Trace: ' . $e->getTraceAsString());
    Log::getInstance()->severe('Terminating...');
}
