/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global define, $, form2js, _, js2form, Handlebars, window */

define("org/forgerock/openam/ui/user/login/RESTLoginView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/login/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function(AbstractView, authNDelegate, validatorsManager, eventManager, constants, conf, sessionManager, router, cookieHelper, uiUtils) {
    
    var LoginView = AbstractView.extend({
        template: "templates/openam/RESTLoginTemplate.html",
        genericTemplate: "templates/openam/RESTLoginTemplate.html",
        unavailableTemplate: "templates/openam/RESTLoginUnavailableTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        data: {},
        events: {
            "click input[type=submit]": "formSubmit"
        },
        formSubmit: function (e) {
            var submitContent,expire;
            
            e.preventDefault();
            submitContent = form2js(this.$el[0]);
            submitContent[$(e.target).attr('name')] = $(e.target).attr('index');
            
            // START CUSTOM STAGE-SPECIFIC LOGIC HERE
            
            // known to be used by DataStore1.html
            if (this.$el.find("[name=loginRemember]:checked").length !== 0) {
                expire = new Date();
                expire.setDate(expire.getDate + 365*20);
                // cheesy assumption that the login name is the first text input box
                cookieHelper.setCookie("login", this.$el.find("input[type=text]:first").val(), expire);
            } else if (this.$el.find("[name=loginRemember]").length !== 0) {
                cookieHelper.deleteCookie("login");
            }
            
            // END CUSTOM STAGE-SPECIFIC LOGIC HERE
            
            eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, submitContent);
        },
        render: function(args, callback) {
            
            if (args && args.length) {
                conf.globalData.auth.realm = args[0];
                conf.globalData.auth.additional = args[1]; // may be "undefined"
            }
            
            authNDelegate.getRequirements()
            .done(_.bind(function (reqs) {
                var cleaned = _.clone(reqs),
                    implicitConfirmation = true;
                
                // if simply by asking for the requirements, we end up with a token, then we must have auto-logged-in somehow
                if (reqs.hasOwnProperty("tokenId")) {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "loggedIn");
                    
                    // if we have a token, let's see who we are logged in as....
                    sessionManager.getLoggedUser(function(user) {
                        conf.setProperty('loggedUser', user);
                        eventManager.sendEvent(constants.EVENT_AUTHENTICATION_DATA_CHANGED, { anonymousMode: false});
                        
                        // copied from EVENT_LOGIN_REQUEST handler
                        if(conf.gotoURL && _.indexOf(["#","","#/","/#"], conf.gotoURL) === -1) {
                            console.log("Auto redirect to " + conf.gotoURL);
                            router.navigate(conf.gotoURL, {trigger: true});
                            delete conf.gotoURL;
                        } else {
                            router.navigate("", {trigger: true});
                        }
                    });
                    
                } else { // we aren't logged in yet, so render a form...
                    
                    cleaned.callbacks = [];
                    _.each(reqs.callbacks, function(element) {
                        
                        if (element.type === "ConfirmationCallback") {
                            implicitConfirmation = false;
                        }
                        
                        cleaned.callbacks.push({
                            input: {
                                index: cleaned.callbacks.length,
                                name: element.input[0].name,
                                value: element.input[0].value
                            },
                            output: element.output,
                            type: element.type,
                            isSubmit: element.type === "ConfirmationCallback"
                        });
                    });
                    
                    if (implicitConfirmation) {
                        cleaned.callbacks.push({
                            "input": {
                                index: cleaned.callbacks.length,
                                name: "loginButton",
                                value: 0
                            },
                            output: [
                                {
                                    name: "options",
                                    value: [ $.t("common.user.login") ]
                                }
                            ],
                            type: "ConfirmationCallback",
                            isSubmit: true
                        });
                    }

                    this.reqs = reqs;
                    this.data.reqs = cleaned;
                    
                    // attempt to load a stage-specific template to render this form.  If not found, use the generic one.
                    uiUtils
                        .fillTemplateWithData("templates/openam/authn/" + reqs.stage + ".html", 
                            _.extend(conf.globalData, this.data),
                            _.bind(function (populatedTemplate) {
                                if (typeof populatedTemplate === "string") { // a rendered template will be a string; an error will be an object
                                    this.template = "templates/openam/authn/" + reqs.stage + ".html";
                                } else {
                                    this.template = this.genericTemplate;
                                }
                                
                                this.parentRender(_.bind(function() {
                                    this.reloadData();
                                }, this));
                            }, this)
                        );
                
                }
            }, this))
            .fail(_.bind(function () {
                // If we can't render a login form, then the user must not be able to login
                this.template = this.unavailableTemplate;
                this.parentRender();
            }, this));

        },
        reloadData: function () {
            // This function is useful for adding logic that is used by stage-specific custom templates.
            
            var login = cookieHelper.getCookie("login");
            
            if(this.$el.find("[name=loginRemember]").length !== 0 && login) {
                this.$el.find("input[type=text]:first").val(login);
                this.$el.find("[name=loginRemember]").attr("checked","true");
                this.$el.find("[type=password]").focus();
            } else {
                this.$el.find(":input:first").focus();
            }

        }
    }); 
    
    Handlebars.registerHelper("callbackRender", function () {
        var result = "",
            cb = this,
            prompt,
            options;
        
        prompt = _.find(cb.output, function (o) { return o.name === "prompt"; });
        if (prompt && prompt.value !== undefined && prompt.value.length) {
            if (cb.type === "ChoiceCallback") {
                result = '<label>' + prompt.value + '</label>';
            } else {
                result = '<label class="short">' + prompt.value + '</label>';
            }
        }
        
        switch (cb.type) {
            case "PasswordCallback" :
                result += '<input type="password" name="callback_' + cb.input.index + '" value="' + cb.input.value + '" data-validator="required" data-validator-event="keyup" />';
            break;
            
            case "TextInputCallback" :
                result += '<textarea name="callback_' + cb.input.index + '" data-validator="required" data-validator-event="keyup">' + cb.input.value + '</textarea>';
            break;
            case "TextOutputCallback" :
                result += '<div id="callback_' + cb.input.index + '" class="textOutputCallback ' + 
                            _.find(cb.output, function (o) { return o.name === "messageType"; }) + '">' + 
                          _.find(cb.output, function (o) { return o.name === "message"; }) + 
                          '</div>';
            break;
            
            case "ConfirmationCallback" : 
                options = _.find(cb.output, function (o) { return o.name === "options"; });
                if (options && options.value !== undefined) {
                    _.each(options.value, function (option, index) {
                        result += '<input name="callback_' + cb.input.index + '" type="submit" class="button active" index="'+ index +'" value="'+ option +'">';
                    });
                }
            break;
            case "ChoiceCallback" : 
                options = _.find(cb.output, function (o) { return o.name === "choices"; });
                if (options && options.value !== undefined) {
                    result += "<ul>";
                    _.each(options.value, function (option, index) {
                        var checked = (cb.input.value === index) ? " checked" : "";
                        result += '<li><label class="short light" for="callback_' + cb.input.index + '_'+ index +'">'+option+': </label><input '+ checked +' id="callback_' + cb.input.index + '_'+ index +'" name="callback_' + cb.input.index + '" type="radio" value="'+ index +'"></li>';
                    });
                    result += "</ul>";
                }
            break;
            default: 
                result += '<input type="text" name="callback_' + cb.input.index + '" value="' + cb.input.value + '" data-validator="required" data-validator-event="keyup" />';
            break;
        }
        
        return new Handlebars.SafeString(result);
    });
    
    
    return new LoginView();
});

