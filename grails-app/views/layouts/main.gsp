<!doctype html>
<html lang="en" class="no-js">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>
        <g:layoutTitle default="iDMED"/>
    </title>
    <meta name="viewport" content="width=device-width, initial-scale=1"/>
    <asset:link rel="icon" href="favicon.ico" type="image/x-ico"/>
    <asset:stylesheet src="application.css"/>

    <g:layoutHead/>
</head>

<body>

<nav class="navbar navbar-expand-lg navbar-dark navbar-static-top centered" role="navigation">
    <div class="container-fluid" align="center">
        <a class="navbar-brand justify-content-center text-md-center" href="/#">
            <asset:image style="height: 100px;width: 110px; text-align: center" src="LogoiDMED.png" alt="iDMED Logo"/>
        </a>
        <a class="text-black-50" style="font-family: 'Fira Sans Condensed ExtraBold'">
            <h5>iDMED BackEND</h5>
        </a>
        <div class="collapse navbar-collapse" aria-expanded="false" style="height: 0.8px;" id="navbarContent">
            <ul class="nav navbar-nav ml-auto">
                <g:pageProperty name="page.nav"/>
            </ul>
        </div>
    </div>
</nav>

<g:layoutBody/>

<div class="footer" role="contentinfo">
    <div class="container-fluid">
        <div class="row">
            <div class="col text-md-center">
                <strong class="centered">
                    <a class="text-black-50" href="#" target="_blank">Version</a>
                </strong>
                <p>2.0.0</p>
            </div>
        </div>
    </div>
</div>

<div id="spinner" class="spinner" style="display:none;">
    <g:message code="spinner.alt" default="Loading&hellip;"/>
</div>

<asset:javascript src="application.js"/>

</body>
</html>
