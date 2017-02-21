package org.openmrs.module.atomfeed.web.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.api.context.Context;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/module/atomfeed/configure.form")
public class ConfigController
{
    protected final Log log = LogFactory.getLog( getClass() );
    
    @RequestMapping(method = RequestMethod.GET)
    public void showConfigForm( ModelMap model )
    {
    }
}
