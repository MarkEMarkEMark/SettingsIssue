precision mediump float;

varying vec2 vTextureCoord;
uniform sampler2D uDiffuseTexture;

//MEO: rgb multipliers
uniform float redMultiplier;
uniform float grnMultiplier;
uniform float bluMultiplier;
uniform float uTileSize;
uniform float uNumTileRows;
uniform int devMode;

vec3 rgb2hsv(vec3 color)
{
    float r, g, b, delta;
    float colorMax, colorMin;
    float h = 0.0, s = 0.0, v = 0.0;
    vec3 hsv = vec3(0.0);
    r = color[0];    
	g = color[1];    
	b = color[2];
    colorMax = max(r,g);    
	colorMax = max(colorMax,b);
    colorMin = min(r,g);    
	colorMin = min(colorMin,b);
    v = colorMax;                // this is value
    
    if(colorMax != 0.0)
    {
      s = (colorMax - colorMin ) / colorMax;
    }
    
    if(s != 0.0)
    {
        delta = colorMax - colorMin;
        if(r == colorMax)
        {
            h = (g - b) / delta;
        }
        else if(g == colorMax)
        {        
            h = 2.0 + (b - r) / delta;
        }
        else // b is max
        {    
            h = 4.0 + (r - g) / delta;
        }
        
        h *= 60.0;
        
        if(h < 0.0)
        {
            h += 360.0;
        }
        
        // this is flawed if the target is 8 bit!!!!!
        hsv[0] = h / 360.0;     // moving h to be between 0 and 1.
        hsv[1] = s;
    }
    
    // DON'T FORGET TO SET VALUE!
    hsv[2] = v;
    
    return hsv.rgb;
}

vec3 hsv2rgb(vec3 hsv)
{
    float h = hsv.x * 6.0; /* H in 0°=0 ... 1=360° - mult by 6 to make colors easier to work with*/
    float s = hsv.y;
    float v = hsv.z;
    float c = v * s;

    vec2 cx = vec2(v*s, c * ( 1.0 - abs(mod(h, 2.0)-1.0) ));

    vec3 rgb = vec3(0.0, 0.0, 0.0);
    if( h < 1.0 ) {
        rgb.rg = cx;
    } else if( h < 2.0 ) {
        rgb.gr = cx;
    } else if( h < 3.0 ) {
        rgb.gb = cx;
    } else if( h < 4.0 ) {
        rgb.bg = cx;
    } else if( h < 5.0 ) {
        rgb.br = cx;
    } else {
        rgb.rb = cx;
    }
    return rgb + vec3(v-cx.y);
}


//expand doubles the level up to 0.5=> 1.0
//for over 0.5 then the saturation is reduced from where it is now (V=0.5) to 0.0 (v=1.0)
vec3 expand(vec3 inputColor) {
	vec3 expanded;
	float newSaturation;
	float newValue;
	vec3 HSV;

	HSV = rgb2hsv(inputColor);
	if (HSV.z < 0.5) { //x = Hue,y = Sat,z = Val ,w= bugger all
		newValue = HSV.z * 2.0; //double value 0.0 to 0.5 => 0.0 to 1.0
		//no need to touch saturation
		newSaturation = HSV.y;
	} else {
		newValue = 1.0; //value is max
		//need to desaturate proportional to level over 0.5 : (1.0 - ((v - 0.5) * 2)) * s
		//newSaturation = 2.0 * (1.0 - HSV.z); - old crap version
		newSaturation = (1.0 - ((HSV.z - 0.5) * 2.0)) * HSV.y;
	}
	expanded = hsv2rgb(vec3(HSV.x, newSaturation, newValue));
	return expanded;
}

vec3 expandWeighted(vec3 inputColor) {
	vec3 expanded;
	float newSaturation;
	float newValue;
	vec3 HSV;

	HSV = rgb2hsv(inputColor);
	float valCorrected = 1.0 - sqrt(1.0 - HSV.z);
	valCorrected = 1.0 - sqrt(1.0 - valCorrected);
	valCorrected = 1.0 - sqrt(1.0 - valCorrected);
	if (valCorrected < 0.5) { //x,y,z,w: z = Value of HSV
		newValue = valCorrected * 2.0;
		newSaturation = 1.0;
	} else {
		newValue = 1.0;
		newSaturation = 2.0 * (1.0 - valCorrected);
	}
	expanded = hsv2rgb(vec3(HSV.x, newSaturation, newValue));

	return expanded;
}

void main() {
	vec4 innerBulb = vec4(expand(vec3(redMultiplier, grnMultiplier, bluMultiplier)), 1.0);
	if (devMode == 0) { //normal mode
		vec2 realTexCoord = vTextureCoord + (gl_PointCoord / uNumTileRows);
		//get the texture pixel
		vec3 inputTexRGB = texture2D(uDiffuseTexture, realTexCoord).rgb;
		
		//Expanded
		//gl_FragColor = vec4(expand(vec3(inputTexRGB.r * redMultiplier, inputTexRGB.g * grnMultiplier, inputTexRGB.b * bluMultiplier)), 1.0);
	
		////Expanded, but weighted towards 1
		//gl_FragColor = vec4(expandWeighted(vec3(inputTexRGB.r * redMultiplier, inputTexRGB.g * grnMultiplier, inputTexRGB.b * bluMultiplier)), 1.0);
	
		// Expanded core, normal outside
		float xDistance = 0.5 - gl_PointCoord.x;
		float yDistance = 0.5 - gl_PointCoord.y;
		float distanceFromCenter = sqrt(xDistance * xDistance + yDistance * yDistance);
		if (distanceFromCenter > 0.066) {
			//halo - black to color - uses texture
			gl_FragColor = vec4(inputTexRGB.r * redMultiplier, inputTexRGB.g * grnMultiplier, inputTexRGB.b * bluMultiplier, 1.0);
		} else {
			// inner bulb - black to color to white - use texture
			// without texture - just expand main color, so like original frag shader
			gl_FragColor = innerBulb;
			//with texture version
			//gl_FragColor = vec4(expand(vec3(inputTexRGB.r * redMultiplier, inputTexRGB.g * grnMultiplier, inputTexRGB.b * bluMultiplier)), 1.0);
		}
	} else { //devloper mode
		vec4 outerBulb = vec4(redMultiplier, grnMultiplier, bluMultiplier, 1.0);
		float xDistance = 0.5 - gl_PointCoord.x;
		if (((gl_PointCoord.x > 0.3333) && (gl_PointCoord.x < 0.6667)) && ((gl_PointCoord.y > 0.3333) && (gl_PointCoord.y < 0.6667))){
			gl_FragColor = innerBulb;
		} else {
			gl_FragColor = outerBulb;
		}
	}
}