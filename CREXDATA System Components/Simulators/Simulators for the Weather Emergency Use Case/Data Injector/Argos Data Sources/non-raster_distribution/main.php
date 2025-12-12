<?php

define('__APP_PATH__', __DIR__);
define('__APP_EXEC__', basename(__FILE__));

// Load classes
require_once(__APP_PATH__ . '/app/autoload.php');


// Application
try {
    $app = new CrexdataNonRasterDistributionApp();

    // The compiler loads the autoload.php classes after the declaration of the new class
    if (EnvironmentHelper::currentEnvironment() != EnvironmentHelper::DEVELOPMENT) {
        SentryHelper::initAndConfigure();
    }

    $app->run();
} catch (Throwable $e) {
    Sentry\captureException($e);
    Log::getInstance()->severe('Exception raised | Msg: '.$e->getMessage().' | File: '.$e->getFile().' | Line: '.$e->getLine());
    Log::getInstance()->severe('Trace: '.$e->getTraceAsString());
    Log::getInstance()->severe('Terminating...');
}
