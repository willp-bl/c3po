/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers;

import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.api.model.Property;
import com.petpet.c3po.utils.Configurator;
import ninja.Result;
import ninja.Results;

import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@Singleton
public class ApplicationController {

    public Result index() {
        return Results.html();
    }

    public Result getProperties() {
        Result html= Results.html();

        PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
        List<String> properties = new ArrayList<String>();

        Iterator<Property> iter = p.find( Property.class, null );
        while ( iter.hasNext() ) {
            properties.add( iter.next().getKey() );
        }
        return Results.json().render(properties);
       // return ok( play.libs.Json.toJson( properties ) );
      //  return Results.html();
    }

    
    public Result helloWorldJson() {
        
        SimplePojo simplePojo = new SimplePojo();
        simplePojo.content = "Hello World! Hello Json!";

        return Results.json().render(simplePojo);

    }
    
    public static class SimplePojo {

        public String content;
        
    }
}
