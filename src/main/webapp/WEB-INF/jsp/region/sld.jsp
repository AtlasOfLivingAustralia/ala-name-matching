<%@ include file="/common/taglibs.jsp"%>
<StyledLayerDescriptor version="1.0.0" xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd" 
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<NamedLayer>
<Name>${param['nl']}</Name>
    <UserStyle>
        <FeatureTypeStyle>
            <Rule>
                <PolygonSymbolizer>
                    <Fill>
                        <CssParameter name="fill">
                            <ogc:Literal>#003300</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="fill-opacity">
                            <ogc:Literal>1.0</ogc:Literal>
                        </CssParameter>
                    </Fill>
                    <Stroke>
                      <CssParameter name="stroke">#006600</CssParameter>
                      <CssParameter name="stroke-width">1</CssParameter>
                    </Stroke>
                </PolygonSymbolizer>
            </Rule>
            <Rule>
              <ogc:Filter xmlns:gml="http://www.opengis.net/gml">
                <ogc:PropertyIsEqualTo>
                  <ogc:PropertyName><string:decodeUrl>${param['pn']}</string:decodeUrl></ogc:PropertyName>
                  <ogc:Literal><string:decodeUrl>${param['pv']}</string:decodeUrl></ogc:Literal>
                </ogc:PropertyIsEqualTo>
              </ogc:Filter>
                <PolygonSymbolizer>
                    <Fill>
                        <CssParameter name="fill">
                            <ogc:Literal>#FF3300</ogc:Literal>
                        </CssParameter>
                        <CssParameter name="fill-opacity">
                            <ogc:Literal>1.0</ogc:Literal>
                        </CssParameter>
                    </Fill>
                </PolygonSymbolizer>
            </Rule>
        </FeatureTypeStyle>
    </UserStyle>
</NamedLayer>
</StyledLayerDescriptor>