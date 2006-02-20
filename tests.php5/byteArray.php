#!/usr/bin/php

<?php
if (!extension_loaded('java')) {
  if (!(PHP_SHLIB_SUFFIX=="so" && dl('java.so'))&&!(PHP_SHLIB_SUFFIX=="dll" && dl('php_java.dll'))) {
    echo "java extension not installed.";
    exit(2);
  }
}

$s=new java("java.lang.String", 12);
$c=$s->getBytes("ASCII");
if($c[0]==ord('1') && $c[1]==ord('2')) {
  echo "test okay\n";
  exit(0);
}
else {
  echo "ERROR\n";
  exit(1);
}
?>