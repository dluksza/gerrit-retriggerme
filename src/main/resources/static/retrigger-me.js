// Copyright (C) 2014 Dariusz Luksza
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

Gerrit.install(function(self) {

  if (window.Polymer) { return; }

  self.onAction('change', 'retrigger', function(c) {
    var submitFn =
           function() {
             c.hide();
             var cng = c.change;
             var patchSetNo = cng.revisions[cng.current_revision]._number;
             c.call({change_no: cng._number,
                     patch_set_no: patchSetNo,
                     change_id: cng.change_id,
                     revision: cng.current_revision}, function(resp) {
                       var msg = "Can't retrigger build because of missing capability: retrigger.";
                       var error = null
                       if (resp) {
                         error = resp['error_message'];
                         if (error) {
                           msg = "Retrigger on '" + resp['jenkins_url'] + "' failed:";
                         } else {
                           msg = "Retriggered on: " + resp['jenkins_url'];
                         }
                       }

                       c.popup(c.div(
                         c.msg(msg),
                         error ? c.span(c.br(), c.msg(error), c.br()) : c.br(),
                         c.button('ok', {onclick: function() {c.hide()}})
                       ));
                     });
           };
    var ok = c.button('Do it!', {onclick: submitFn});
    var cancel = c.button('Not really', {onclick: function() {c.hide()}});
    c.popup(c.div(
      c.msg("Really retrigger?"),
      c.br(),
      c.span(cancel, ok)
    ));
  });
});

