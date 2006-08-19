<?php /*-*- mode: php; tab-width:4 -*-*/

  /* java_NativeParser.php -- PHP/Java Bridge protocol parser.

  Copyright (C) 2006 Jost Boekemeier

  This file is part of the PHP/Java Bridge.

  This file ("the library") is free software; you can redistribute it
  and/or modify it under the terms of the GNU General Public License as
  published by the Free Software Foundation; either version 2, or (at
  your option) any later version.

  The library is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with the PHP/Java Bridge; see the file COPYING.  If not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
  02111-1307 USA.

  Linking this file statically or dynamically with other modules is
  making a combined work based on this library.  Thus, the terms and
  conditions of the GNU General Public License cover the whole
  combination.

  As a special exception, the copyright holders of this library give you
  permission to link this library with independent modules to produce an
  executable, regardless of the license terms of these independent
  modules, and to copy and distribute the resulting executable under
  terms of your choice, provided that you also meet, for each linked
  independent module, the terms and conditions of the license of that
  module.  An independent module is a module which is not derived from
  or based on this library.  If you modify this library, you may extend
  this exception to your version of the library, but you are not
  obligated to do so.  If you do not wish to do so, delete this
  exception statement from your version. */

class java_NativeParser {
  var $parser, $handler;
  var $level, $event;

  function java_NativeParser($handler) {
	$this->handler = $handler;
	$this->parser = xml_parser_create();
	xml_parser_set_option($this->parser, XML_OPTION_CASE_FOLDING, 0);
	xml_set_object($this->parser, $this);
	xml_set_element_handler($this->parser, "begin", "end");
	xml_parse($this->parser, "<F>");
	$this->level = 0;
  }
  function begin($parser, $name, $param) {
	$this->event = true;
	switch($name) {
	case 'X': case 'A': $this->level++;
	}
	$this->handler->begin($name, $param);
  }
  function end($parser, $name) {
	$this->handler->end($name);
	switch($name) {
	case 'X': case 'A': $this->level--;
	}
  }
  function getData($str) {
	return base64_decode($str);
  }
  function parse() {
	// NOTE: RECV_SIZE must be the same as the underlying 
	// stream buffer size, otherwise this function may hang.
	do {
	  $this->event = false;
	  $buf=$this->handler->read($this->handler->RECV_SIZE); 
	  if(!xml_parse($this->parser, $buf)) {
		die(sprintf("protocol error: %s, %s at col %d",
					$buf,
					xml_error_string(xml_get_error_code($this->parser)),
					xml_get_current_column_number($this->parser)));
	  }
	} while(!$this->event || $this->level>0);
  }

  function __destruct() {
	xml_parser_free($this->parser);
  }
}
?>
