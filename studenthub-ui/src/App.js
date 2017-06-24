// 'base' imports
import React, { Component } from 'react';
import { BrowserRouter as Router, Route, Link, Switch, Redirect, withRouter } from 'react-router-dom';

// views
import TopicSearch from './views/TopicSearch.js';
import SignIn from './views/SignIn.js';
import SignUp from './views/SignUp.js';
import Activation from './views/Activation.js';
import ForgotPassword from './views/ForgotPassword.js';
import Users from './views/Users.js';
import Universities from './views/Universities.js';
import Companies from './views/Companies.js';
import MyTopics from './views/MyTopics.js';
import Topic from './views/Topic.js';
import MyApplications from './views/MyApplications.js';
import Application from './views/Application.js';
import Profile from './views/Profile.js';
import CompanyReg from './views/CompanyReg.js';

// components
import AboutDrawer from './components/AboutDrawer.js';
import Button from 'react-toolbox/lib/button/Button.js';
import AppBar from 'react-toolbox/lib/app_bar/AppBar.js';
import Navigation from 'react-toolbox/lib/navigation/Navigation.js';
import FontIcon from 'react-toolbox/lib/font_icon/FontIcon.js';

// Auth controller
import Auth from './Auth.js';
import Util from './Util.js';
import _t from './Translations.js';

// set page title
document.title = Util.PORTAL_NAME;

/**
 * '404' page - displayed when no route is matched
 */
const NoMatch = ({ location }) => (
  <div className='text-center'>
    <h3>
      <FontIcon value='error_outline' /> { _t.translate("No page available at location:") } <code>{location.pathname}</code>
    </h3>
  </div>
)

/**
 * Component to display navigation links in AppBar
 */
const NavBarLinks = withRouter(() => (
  <div id="topNavBar">
    <Link to="/"><Button label={ _t.translate("Home") } flat /></Link>
    { Auth.hasRole(Util.userRoles.admin) ? <Link to="/users"><Button label={ _t.translate("Users") } flat /></Link> : '' }
    { Auth.hasRole(Util.userRoles.admin) ? <Link to="/unis"><Button label={ _t.translate("Universities") } flat /></Link> : '' }
    { Auth.hasRole(Util.userRoles.admin) ? <Link to="/companies"><Button label={ _t.translate("Companies") } flat /></Link> : '' }
    { Auth.isAuthenticated() ? <Link to="/my-apps"><Button label={ _t.translate("My Applications") } flat /></Link> : '' }
    { Auth.hasRole(Util.userRoles.techLeader) || Auth.hasRole(Util.userRoles.superviser) ? <Link to="/my-topics"><Button label={ _t.translate("My Topics") } flat /></Link> : '' }
    {
      Auth.isAuthenticated() ?
        <Link to="/profile">
          <Button label={ _t.translate("Profile") } icon="account_circle" flat />
        </Link>
        :
        <Link to="/signin">
          <Button label={ _t.translate("Sign In") } icon="account_circle" flat />
        </Link>
    }
  </div>
))

/**
 * Route extension to enforce auth
 */
const PrivateRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render={props => (
    Auth.isAuthenticated() ? (
      <Component {...props}/>
    ) : (
      <Redirect to={{
        pathname: '/signin',
        state: { from: props.location }
      }}/>
    )
  )}/>
)

/**
* Main application comonent
*/
class App extends Component {

  state = { drawerActive: false }

  render() {
    return (
      <Router>
        <div>
          <AppBar title={ Util.PORTAL_NAME } leftIcon='menu' onLeftIconClick={() => this.setState({ drawerActive: true })}>
            <Navigation type='horizontal'>
              <NavBarLinks />
            </Navigation>
            <AboutDrawer active={ this.state.drawerActive } onOverlayClick={() => this.setState({ drawerActive: false })}/>
          </AppBar>

          { /* Routes definition */ }
          <div className='container'>
            <Switch>
              <Route exact path="/" component={TopicSearch}/>
              <Route exact path="/signin" component={SignIn}/>
              <Route exact path="/signup" component={SignUp}/>
              <Route exact path="/forgot" component={ForgotPassword}/>
              <Route exact path="/activation/:id/:secret" component={Activation}/>
              <Route exact path="/topics/:id" component={Topic}/>
              <PrivateRoute exact path="/users" component={Users}/>
              <PrivateRoute exact path="/unis" component={Universities}/>
              <PrivateRoute exact path="/companies" component={Companies}/>
              <PrivateRoute exact path="/my-apps" component={MyApplications}/>
              <PrivateRoute exact path="/my-topics" component={MyTopics}/>
              <PrivateRoute exact path="/profile" component={Profile}/>
              <PrivateRoute path="/applications/:id" component={Application}/>
              <Route exact path="/company-reg" component={CompanyReg}/>
              <Route component={NoMatch}/>
            </Switch>
          </div>
        </div>
      </Router>
    );
  }
}

export default App;
