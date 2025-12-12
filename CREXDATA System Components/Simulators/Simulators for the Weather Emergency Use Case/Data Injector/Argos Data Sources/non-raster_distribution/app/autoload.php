<?php

function hydsApplicationsCustomAutoLoad($class_name)
{
    // Search recursively in the "app" directory
    if (file_exists(__APP_PATH__ . '/app/' . $class_name . '.class.php')) {
        require_once(__APP_PATH__ . '/app/' . $class_name . '.class.php');
        return;
    }
    $iter = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator(__APP_PATH__ . '/app', RecursiveDirectoryIterator::SKIP_DOTS),
        RecursiveIteratorIterator::SELF_FIRST,
        RecursiveIteratorIterator::CATCH_GET_CHILD /* Ignore "Permission denied" */
    );
    foreach ($iter as $path => $dir) {
        if ($dir->isDir() && file_exists($dir . '/' . $class_name . '.class.php')) {
            require_once($dir . '/' . $class_name . '.class.php');
            break;
        }
    }


    // Shared libraries
    $lib_path = getenv('LIB_PATH');
    if (!$lib_path) {
        die("Environment variable for LIB_PATH not found." . PHP_EOL);
    }
    if (file_exists($lib_path . '/' . $class_name . '.class.php')) {
        require_once($lib_path . '/' . $class_name . '.class.php');
        return;
    }
    $iter = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($lib_path, RecursiveDirectoryIterator::SKIP_DOTS),
        RecursiveIteratorIterator::SELF_FIRST,
        RecursiveIteratorIterator::CATCH_GET_CHILD /* Ignore "Permission denied" */
    );
    foreach ($iter as $path => $dir) {
        if ($dir->isDir() && file_exists($dir . '/' . $class_name . '.class.php')) {
            require_once($dir . '/' . $class_name . '.class.php');
            break;
        }
    }

    // Composer packages
    if (is_file($lib_path . '/vendor/autoload.php')) {
        require_once($lib_path . '/vendor/autoload.php');
    }
}

spl_autoload_register('hydsApplicationsCustomAutoLoad');
