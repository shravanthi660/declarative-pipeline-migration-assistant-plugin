package io.jenkins.plugins.todeclarative.converter.buildwrapper;

import hudson.Extension;
import hudson.tasks.BuildWrapper;
import io.jenkins.plugins.todeclarative.converter.ConverterRequest;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.credentialsbinding.Binding;
import org.jenkinsci.plugins.credentialsbinding.MultiBinding;
import org.jenkinsci.plugins.credentialsbinding.impl.SecretBuildWrapper;
import org.jenkinsci.plugins.credentialsbinding.impl.UsernamePasswordMultiBinding;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTKey;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTNamedArgumentList;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTSingleArgument;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTStage;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTTreeStep;
import org.jenkinsci.plugins.pipeline.modeldefinition.ast.ModelASTValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Extension
public class SecretBuildWrapperConverter
    implements BuildWrapperConverter
{
    @Override
    public ModelASTStage convert( ConverterRequest request, BuildWrapper wrapper )
    {
        SecretBuildWrapper secretBuildWrapper = SecretBuildWrapper.class.cast( wrapper );
        if ( secretBuildWrapper.getBindings() == null || secretBuildWrapper.getBindings().isEmpty() )
        {
            return null;
        }

        ModelASTTreeStep withCredentials = new ModelASTTreeStep( this );
        request.setWithCredentials( withCredentials );
        withCredentials.setName( "withCredentials" );
        ModelASTSingleArgument singleArgument = new ModelASTSingleArgument( null);
        withCredentials.setArgs( singleArgument );

        for( MultiBinding binding : secretBuildWrapper.getBindings() ){
            String credentialId = binding.getCredentialsId();
            Symbol symbolAnn = binding.getDescriptor().getClass().getAnnotation( Symbol.class );
            List<String> symbols = Arrays.asList(symbolAnn.value());
            for(String symbol : symbols){

                switch ( symbol ) {
                    case "usernamePassword":
                        //withCredentials([usernamePassword(usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD', credentialsId: 'creds')])
                        UsernamePasswordMultiBinding usernamePasswordMultiBinding =
                            UsernamePasswordMultiBinding.class.cast( binding );
                        usernamePasswordMultiBinding.getPasswordVariable();

                        // FIXME we need something with java code to generate this...
                        StringBuilder gstring = new StringBuilder( "[usernamePassword(usernameVariable:'" );
                        gstring.append( usernamePasswordMultiBinding.getUsernameVariable());
                        gstring.append( "', passwordVariable: '" );
                        gstring.append( usernamePasswordMultiBinding.getPasswordVariable() );
                        gstring.append( "', credentialsId: '" );
                        gstring.append( credentialId );
                        gstring.append( "')]" );

                        //ModelASTValue{value=${[usernameColonPassword(credentialsId: 'mylogin', variable: 'USERPASS')]}, isLiteral=false}
                        singleArgument.setValue( ModelASTValue.fromGString( gstring.toString(), this ) );


                     default:
                         // FIXME warning in result
                         System.out.println( "ignore symbol:" +  symbol );

                }
            }
        }

        return null;
    }

    @Override
    public boolean canConvert( BuildWrapper wrapper )
    {
        return wrapper instanceof SecretBuildWrapper;
    }
}