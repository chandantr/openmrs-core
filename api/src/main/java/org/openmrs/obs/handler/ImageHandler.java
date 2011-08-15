/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.obs.handler;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Obs;
import org.openmrs.api.APIException;
import org.openmrs.obs.ComplexData;
import org.openmrs.obs.ComplexObsHandler;
import org.openmrs.util.OpenmrsConstants;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Handler for storing basic images for complex obs to the file system. The image mime type used is
 * taken from the image name. if the .* image name suffix matches
 * {@link javax.imageio.ImageIO#getWriterFormatNames()} then that mime type will be used to save the
 * image. Images are stored in the location specified by the global property: "obs.complex_obs_dir"
 * 
 * @see OpenmrsConstants#GLOBAL_PROPERTY_COMPLEX_OBS_DIR
 * @since 1.5
 * 
 * There may be several classes which extend
 * ImageHandler. Out of these, only one will be loaded by Spring. The class to be loaded will be
 * decided based on the @Order annotation value. 
 * 
 * As default, ImageHandler will have the lowest possible
 * priority.
 * 
 */

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ImageHandler extends AbstractHandler implements ComplexObsHandler {
	
	public static final Log log = LogFactory.getLog(ImageHandler.class);
	
	/** The Constant HANDLER_TYPE. Used to differentiate between handler types*/
	public static final String HANDLER_TYPE = "ImageHandler";
	
	private Set<String> extensions;
	
	/**
	 * Constructor initializes formats for alternative file names to protect from unintentionally
	 * overwriting existing files.
	 */
	public ImageHandler() {
		super();
		
		// Create a HashSet to quickly check for supported extensions.
		extensions = new HashSet<String>();
		for (String mt : ImageIO.getWriterFormatNames()) {
			extensions.add(mt);
		}
	}
	
	/**
	 * Currently supports all views and puts the Image file data into the ComplexData object.
	 *
	 * @see org.openmrs.obs.ComplexObsHandler#getObs(org.openmrs.Obs, java.lang.String)
	 */
	public Obs getObs(Obs obs, String view) {
		File file = getComplexDataFile(obs);
		BufferedImage img = null;
		try {
			img = ImageIO.read(file);
		}
		catch (IOException e) {
			log.error("Trying to read file: " + file.getAbsolutePath(), e);
		}
		
		ComplexData complexData = new ComplexData(file.getName(), img);
		
		obs.setComplexData(complexData);
		
		return obs;
	}
	
	/**
	 * @see org.openmrs.obs.ComplexObsHandler#saveObs(org.openmrs.Obs)
	 */
	public Obs saveObs(Obs obs) throws APIException {
		// Get the buffered image from the ComplexData.
		BufferedImage img = null;
		
		Object data = obs.getComplexData().getData();
		if (BufferedImage.class.isAssignableFrom(data.getClass())) {
			img = (BufferedImage) obs.getComplexData().getData();
		} else if (InputStream.class.isAssignableFrom(data.getClass())) {
			try {
				img = ImageIO.read((InputStream) data);
			}
			catch (IOException e) {
				throw new APIException(
				        "Unable to convert complex data to a valid input stream and then read it into a buffered image");
			}
		}
		
		if (img == null) {
			throw new APIException("Cannot save complex obs where obsId=" + obs.getObsId()
			        + " because its ComplexData.getData() is null.");
		}
		
		try {
			File outfile = getOutputFileToWrite(obs);
			
			String extension = getExtension(obs.getComplexData().getTitle());
			
			// TODO: Check this extension against the registered extensions for validity
			
			// Write the file to the file system.
			ImageIO.write(img, extension, outfile);
			
			// Set the Title and URI for the valueComplex
			obs.setValueComplex(extension + " image |" + outfile.getName());
			
			// Remove the ComlexData from the Obs
			obs.setComplexData(null);
			
		}
		catch (IOException ioe) {
			throw new APIException("Trying to write complex obs to the file system. ", ioe);
		}
		
		return obs;
	}
	
	/**
	 * Gets the handler type for each registered handler.
	 * 
	 * @return the handler type
	 */
	@Override
	public String getHandlerType() {
		return ImageHandler.HANDLER_TYPE;
	}
	
	/**
	 * Validate.
	 */
	@Override
	public boolean validate(String handlerConfig, Obs obs) {
		return true;
	}
	
	/**
	 * This method is used to return the persisted data only. The image is retrieved
	 * using data from the Obs passed in. This is returned to the user. If there is no
	 * image, then the method returns null.
	 */
	@Override
	public Object getValue(Obs obs) {
		//Returns null, since this is unnessesary for now.
		return null;
	}
	
}