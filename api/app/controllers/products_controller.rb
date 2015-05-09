class ProductsController < ApplicationController

  def index
      token = ::Sphereio.login
      project_key = 'matchit-15'
      headers = { 'Authorization' => "Bearer #{token}" }
      res = Excon.get "https://api.sphere.io/#{project_key}/product-projections", :headers => headers
      products = JSON.parse res.body
      render json: products
  end
end